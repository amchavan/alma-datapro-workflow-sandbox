from setuptools import setup

dependencies = [
    "adaptmb-messages",
    "adaptmb"
]

setup(name='adapt-mock-pipeline',
      version='0.1',
      description='DRAWS Mock Pipeline',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['adapt', 'adapt/mock', 'adapt/mock/pipeline'],
      install_requires=dependencies,
      zip_safe=False)
