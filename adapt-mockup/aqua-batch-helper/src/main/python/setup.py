from setuptools import setup

dependencies = [
    "adaptmb-messages",
    "adaptmb"
]

setup(name='adapt-mock-aqua-batch-helper',
      version='0.1',
      description='DRAWS Mock Aqua Batch Helper',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['adapt', 'adapt/mock', 'adapt/mock/aqua'],
      install_requires=dependencies,
      zip_safe=False)
