from setuptools import setup

dependencies = [
    "adaptmb-messages",
    "adaptmb"
]

setup(name='adapt-mock-pldriver',
      version='0.1',
      description='DRAWS Mock Pipeline Driver',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['adapt', 'adapt/mock', 'adapt/mock/pldriver', 'adapt/mock/pldriver/tasks'],
      install_requires=dependencies,
      zip_safe=False)
